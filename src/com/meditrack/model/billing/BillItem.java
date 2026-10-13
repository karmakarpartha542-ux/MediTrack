package com.meditrack.model.billing;

import com.meditrack.util.JsonUtils;

/**
 * Line item on a patient hospital invoice detailing a specific service or accommodation fee.
 */
public class BillItem {

    private String description;
    private double amount;

    public BillItem() {
        this.description = "";
        this.amount = 0.0;
    }

    public BillItem(String description, double amount) {
        this.description = description != null ? description.trim() : "";
        this.amount = Math.max(0.0, amount);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : "";
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = Math.max(0.0, amount);
    }

    public String toJson() {
        return "{"
                + "\"description\":\"" + JsonUtils.escape(description) + "\","
                + "\"amount\":" + amount
                + "}";
    }

    public static BillItem fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        String desc = JsonUtils.getString(json, "description");
        double amt = JsonUtils.getDouble(json, "amount");
        return new BillItem(desc, amt);
    }

    @Override
    public String toString() {
        return "BillItem{" +
                "description='" + description + '\'' +
                ", amount=" + amount +
                '}';
    }
}
