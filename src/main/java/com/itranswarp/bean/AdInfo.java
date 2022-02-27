package com.itranswarp.bean;

import java.util.ArrayList;
import java.util.List;

import com.itranswarp.model.AdMaterial;
import com.itranswarp.model.AdSlot;

public class AdInfo {

    public List<AdSlotInfo> slots = new ArrayList<>();

    public AdSlotInfo addAdSlot(AdSlot slot) {
        AdSlotInfo s = new AdSlotInfo(slot);
        slots.add(s);
        return s;
    }

    public static class AdSlotInfo {

        public String alias;
        public long width;
        public long height;
        public long numSlots;
        public long numAutoFill;
        public String adAutoFill;

        public List<AdPeriodInfo> periods = new ArrayList<>();

        public AdSlotInfo() {
        }

        public AdSlotInfo(AdSlot slot) {
            this.alias = slot.alias;
            this.width = slot.width;
            this.height = slot.height;
            this.numSlots = slot.numSlots;
            this.numAutoFill = slot.numAutoFill;
            this.adAutoFill = slot.adAutoFill;
        }

        public void addAdPeriodWithAdMaterials(List<AdMaterial> materials) {
            AdPeriodInfo period = new AdPeriodInfo();
            for (AdMaterial material : materials) {
                period.addAdMaterial(material);
            }
            this.periods.add(period);
        }
    }

    public static class AdPeriodInfo {

        public List<AdMaterialInfo> materials = new ArrayList<>();

        public AdPeriodInfo() {
        }

        public void addAdMaterial(AdMaterial material) {
            this.materials.add(new AdMaterialInfo(material));
        }
    }

    public static class AdMaterialInfo {

        public long imageId;
        public long weight;
        public String[] tags;
        public String url;

        public AdMaterialInfo() {
        }

        public AdMaterialInfo(AdMaterial material) {
            this.imageId = material.imageId;
            this.weight = material.weight;
            this.url = material.url;
            this.tags = material.tags.split(",");
        }
    }
}
