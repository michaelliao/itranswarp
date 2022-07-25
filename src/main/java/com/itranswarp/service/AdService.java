package com.itranswarp.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.itranswarp.bean.AdInfo;
import com.itranswarp.bean.AdInfo.AdSlotInfo;
import com.itranswarp.bean.AdMaterialBean;
import com.itranswarp.bean.AdPeriodBean;
import com.itranswarp.bean.AdSlotBean;
import com.itranswarp.bean.AttachmentBean;
import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.enums.Role;
import com.itranswarp.model.AdMaterial;
import com.itranswarp.model.AdPeriod;
import com.itranswarp.model.AdSlot;
import com.itranswarp.model.User;
import com.itranswarp.warpdb.PagedResults;

@Component
public class AdService extends AbstractDbService<AdSlot> {

    @Autowired
    UserService userService;

    @Autowired
    AttachmentService attachmentService;

    static final String KEY_AD = "__ad__";

    public void deleteAdInfoFromCache() {
        this.redisService.del(KEY_AD);
    }

    public AdInfo getAdInfoFromCache() {
        AdInfo adInfo = this.redisService.get(KEY_AD, AdInfo.class);
        if (adInfo == null) {
            final AdInfo ad = new AdInfo();
            LocalDate today = LocalDate.now();
            String todayStr = today.toString();
            List<AdSlot> adSlots = getAdSlots();
            adSlots.forEach(adSlot -> {
                AdSlotInfo slot = ad.addAdSlot(adSlot);
                List<AdPeriod> adPeriods = getActiveAdPeriodsByAdSlot(adSlot, todayStr);
                adPeriods.forEach(period -> {
                    List<AdMaterial> adMaterials = getActiveAdMaterialsByAdPeriod(period, todayStr);
                    slot.addAdPeriodWithAdMaterials(adMaterials);
                });
            });
            adInfo = ad;
            long ts = System.currentTimeMillis() / 1000;
            long endOfToday = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
            long expires = endOfToday - ts;
            if (expires > 3) {
                this.redisService.set(KEY_AD, ad, expires);
            }
        }
        return adInfo;
    }

    public List<AdSlot> getAdSlots() {
        return db.from(AdSlot.class).orderBy("name").list();
    }

    @Transactional
    public void deleteAdSlot(long id) {
        AdSlot slot = this.getById(id);
        String endAt = LocalDate.now().toString();
        AdPeriod period = db.from(AdPeriod.class).where("adSlotId = ? AND endAt >= ?", slot.id, endAt).first();
        if (period != null) {
            throw new ApiException(ApiError.OPERATION_FAILED, "adSlot", "Could not delete adslot with active adperiod.");
        }
        this.db.remove(slot);
    }

    @Transactional
    public AdSlot createAdSlot(AdSlotBean bean) {
        bean.validate(true);
        AdSlot slot = new AdSlot();
        slot.copyPropertiesFrom(bean);
        this.db.insert(slot);
        return slot;
    }

    @Transactional
    public AdSlot updateAdSlot(long id, AdSlotBean bean) {
        bean.validate(false);
        AdSlot slot = this.getById(id);
        slot.name = bean.name;
        slot.description = bean.description;
        slot.price = bean.price;
        slot.numAutoFill = bean.numAutoFill;
        slot.adAutoFill = bean.adAutoFill;
        slot.numSlots = bean.numSlots;
        this.db.update(slot);
        return slot;
    }

    public AdPeriod getAdPeriodById(long id) {
        AdPeriod ap = this.db.fetch(AdPeriod.class, id);
        if (ap == null) {
            throw new ApiException(ApiError.ENTITY_NOT_FOUND, "AdPeriod", "AdPeriod not found.");
        }
        return ap;
    }

    public List<AdPeriod> getAdPeriods() {
        return this.db.from(AdPeriod.class).orderBy("endAt").desc().orderBy("startAt").desc().orderBy("displayOrder").orderBy("id").list();
    }

    @Transactional
    public AdPeriod createAdPeriod(AdPeriodBean bean) {
        bean.validate(true);
        User user = userService.getById(bean.userId);
        if (user.role != Role.SPONSOR) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "userId", "Not a sponser user.");
        }
        AdSlot slot = this.getById(bean.adSlotId);
        List<AdPeriod> all = getAdPeriodsByAdSlot(slot);
        if (all.size() >= slot.numSlots) {
            throw new ApiException(ApiError.OPERATION_FAILED, "AdPeriod", "Cannot create more AdPeriod");
        }
        long maxDisplayOrder = all.stream().mapToLong(ap -> ap.displayOrder).max().orElseGet(() -> 0L);
        AdPeriod ap = new AdPeriod();
        ap.copyPropertiesFrom(bean);
        ap.displayOrder = maxDisplayOrder + 1;
        this.db.insert(ap);
        return ap;
    }

    @Transactional
    public AdPeriod updateAdPeriod(long id, AdPeriodBean bean) {
        bean.validate(false);
        AdPeriod ap = getAdPeriodById(id);
        if (isExpiredAdPeriod(ap)) {
            throw new ApiException(ApiError.OPERATION_FAILED, "AdPeriod", "Could not update expired AdPeriod.");
        }
        ap.endAt = bean.endAt;
        if (ap.endAt.compareTo(ap.startAt) <= 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "endAt", "Invalid endAt.");
        }
        this.db.update(ap);
        return ap;
    }

    @Transactional
    public void deleteAdPeriod(long id) {
        AdPeriod ap = getAdPeriodById(id);
        if (!isExpiredAdPeriod(ap)) {
            throw new ApiException(ApiError.OPERATION_FAILED, "AdPeriod", "Could not delete non-expired AdPeriod.");
        }
        List<AdMaterial> ms = getAdMaterialsByAdPeriod(ap);
        this.db.remove(ms);
        this.db.remove(ap);
    }

    public PagedResults<AdMaterial> getAdMaterials(int pageIndex) {
        return this.db.from(AdMaterial.class).orderBy("createdAt").desc().list(pageIndex, ITEMS_PER_PAGE);
    }

    @Transactional
    public AdMaterial createAdMaterial(User user, AdPeriod ap, AdMaterialBean bean) {
        if (user.id != ap.userId) {
            throw new ApiException(ApiError.PERMISSION_DENIED, "userId", "Invalid user.");
        }
        if (isExpiredAdPeriod(ap)) {
            throw new ApiException(ApiError.OPERATION_FAILED, "AdPeriod", "Could not create AdMaterial for expired AdPeriod.");
        }
        bean.validate(true);
        if (this.db.from(AdMaterial.class).where("adPeriodId = ?", ap.id).count() >= 10) {
            throw new ApiException(ApiError.OPERATION_FAILED, "AdMaterial", "Could not create more AdMaterial.");
        }
        AdMaterial m = new AdMaterial();
        m.copyPropertiesFrom(bean);
        m.adPeriodId = ap.id;

        AttachmentBean atta = new AttachmentBean();
        atta.name = user.name;
        atta.data = bean.image;
        m.imageId = attachmentService.createAttachment(user, atta).id;
        this.db.insert(m);
        return m;
    }

    @Transactional
    public void deleteAdMaterial(User user, long adMaterialId) {
        AdMaterial m = this.db.fetch(AdMaterial.class, adMaterialId);
        if (m == null) {
            throw new ApiException(ApiError.ENTITY_NOT_FOUND, "AdMaterial", "Entity not found by id.");
        }
        AdPeriod p = this.db.fetch(AdPeriod.class, m.adPeriodId);
        if (p == null || user.role != Role.ADMIN && p.userId != user.id) {
            throw new ApiException(ApiError.ENTITY_NOT_FOUND, "AdMaterial", "Entity not found by id.");
        }
        this.db.remove(m);
    }

    public List<AdPeriod> getAdPeriodsByAdSlot(AdSlot slot) {
        return this.db.from(AdPeriod.class).where("adSlotId = ?", slot.id).orderBy("endAt").desc().list();
    }

    public List<AdPeriod> getAdPeriodsByUser(User user) {
        return this.db.from(AdPeriod.class).where("userId = ?", user.id).orderBy("adSlotId").orderBy("displayOrder").orderBy("endAt").desc().list();
    }

    private List<AdPeriod> getActiveAdPeriodsByAdSlot(AdSlot slot, String today) {
        return this.db.from(AdPeriod.class).where("adSlotId = ? AND startAt <= ? AND endAt > ?", slot.id, today, today).orderBy("displayOrder").list();
    }

    public List<AdMaterial> getAdMaterialsByAdPeriod(AdPeriod period) {
        return this.db.from(AdMaterial.class).where("adPeriodId = ?", period.id).orderBy("endAt").desc().list();
    }

    private List<AdMaterial> getActiveAdMaterialsByAdPeriod(AdPeriod period, String today) {
        return this.db.from(AdMaterial.class).where("adPeriodId = ? AND startAt <= ? AND endAt > ?", period.id, today, today).orderBy("endAt").desc().list();
    }

    private boolean isExpiredAdPeriod(AdPeriod ap) {
        String today = LocalDate.now().toString();
        return ap.endAt.compareTo(today) <= 0;
    }

    static final TypeReference<List<AdSlot>> TYPE_LIST_ADSLOT = new TypeReference<>() {
    };

}
