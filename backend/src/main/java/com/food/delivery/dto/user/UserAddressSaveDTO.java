package com.food.delivery.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class UserAddressSaveDTO {
    @NotBlank(message = "contactName required")
    @Size(max = 64)
    private String contactName;

    @NotBlank(message = "contactPhone required")
    @Pattern(regexp = "^1\\d{10}$", message = "contactPhone format invalid")
    private String contactPhone;

    @NotBlank(message = "province required")
    @Size(max = 32)
    private String province;

    @NotBlank(message = "city required")
    @Size(max = 32)
    private String city;

    @NotBlank(message = "district required")
    @Size(max = 32)
    private String district;

    @NotBlank(message = "detail required")
    @Size(max = 255)
    private String detail;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @NotNull(message = "isDefault required")
    private Integer isDefault;

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }
}
