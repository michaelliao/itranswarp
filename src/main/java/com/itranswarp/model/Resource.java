package com.itranswarp.model;

import java.util.Base64;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.enums.ResourceEncoding;

/**
 * Store any uploaded files. Usually using base64 encoding.
 * 
 * @author liaoxuefeng
 */
@Entity
@Table(name = "resources", uniqueConstraints = @UniqueConstraint(name = "UNI_HASH", columnNames = { "hash" }))
public class Resource extends AbstractEntity {

    /**
     * Hash of the binary data.
     */
    @Column(nullable = false, updatable = false, length = VAR_CHAR_HASH)
    public String hash;

    /**
     * Content encoding. e.g. "BASE64".
     */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public ResourceEncoding encoding;

    @Column(nullable = false, updatable = false, columnDefinition = "MEDIUMTEXT")
    public String content;

    public byte[] decode() {
        if (encoding == ResourceEncoding.BASE64) {
            return Base64.getDecoder().decode(this.content);
        }
        throw new ApiException(ApiError.OPERATION_FAILED, null, "Could not decode content data.");
    }

}
