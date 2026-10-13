package com.meditrack.model.billing;

import com.meditrack.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Hospital invoice encapsulating billing transactions for patient care,
 * room stay, procedures, and doctor consultations.
 */
public class Bill {

    private String billId;
    private String patientId;
    private String patientName;
    private List<BillItem> items;
    private double totalAmount;
    private boolean isPaid;
    private String createdDate;
    private String notes;

    /**
     * Default no-arg constructor.
     */
    public Bill() {
        this.items = new ArrayList<>();
        this.totalAmount = 0.0;
        this.isPaid = false;
        this.createdDate = "";
        this.notes = "";
    }

    /**
     * Convenience constructor.
     */
    public Bill(String billId, String patientId, String patientName, String createdDate, String notes) {
        this.billId = billId != null ? billId.trim() : "";
        this.patientId = patientId != null ? patientId.trim() : "";
        this.patientName = patientName != null ? patientName.trim() : "";
        this.items = new ArrayList<>();
        this.totalAmount = 0.0;
        this.isPaid = false;
        this.createdDate = createdDate != null ? createdDate.trim() : "";
        this.notes = notes != null ? notes.trim() : "";
    }

    /**
     * Parameterized constructor with all fields.
     */
    public Bill(String billId, String patientId, String patientName, List<BillItem> items,
                double totalAmount, boolean isPaid, String createdDate, String notes) {
        this.billId = billId != null ? billId.trim() : "";
        this.patientId = patientId != null ? patientId.trim() : "";
        this.patientName = patientName != null ? patientName.trim() : "";
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.totalAmount = Math.max(0.0, totalAmount);
        this.isPaid = isPaid;
        this.createdDate = createdDate != null ? createdDate.trim() : "";
        this.notes = notes != null ? notes.trim() : "";
    }

    /**
     * Adds a line item to this bill and recalculates total invoice amount.
     *
     * @param item billable item to append
     */
    public void addItem(BillItem item) {
        if (item != null) {
            this.items.add(item);
            recalculateTotal();
        }
    }

    /**
     * Recalculates the total amount based on the sum of all item charges.
     */
    public void recalculateTotal() {
        double sum = 0.0;
        for (BillItem item : items) {
            sum += item.getAmount();
        }
        this.totalAmount = Math.round(sum * 100.0) / 100.0;
    }

    /**
     * Applies a discount percentage to the total invoice amount.
     *
     * @param percent discount percentage between 0.0 and 100.0
     * @throws IllegalArgumentException if discount percentage is outside [0, 100]
     */
    public void applyDiscount(double percent) {
        if (percent < 0.0 || percent > 100.0) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100.");
        }
        double discounted = this.totalAmount * (1.0 - (percent / 100.0));
        this.totalAmount = Math.max(0.0, Math.round(discounted * 100.0) / 100.0);
    }

    /**
     * Marks the invoice as paid.
     */
    public void markPaid() {
        this.isPaid = true;
    }

    // ------------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------------

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId != null ? billId.trim() : "";
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId != null ? patientId.trim() : "";
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName != null ? patientName.trim() : "";
    }

    public List<BillItem> getItems() {
        return items;
    }

    public void setItems(List<BillItem> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        recalculateTotal();
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = Math.max(0.0, totalAmount);
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        this.isPaid = paid;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate != null ? createdDate.trim() : "";
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes.trim() : "";
    }

    /**
     * Serializes this bill including nested BillItems array into JSON format.
     *
     * @return JSON string
     */
    public String toJson() {
        StringBuilder itemsJson = new StringBuilder();
        itemsJson.append("[");
        for (int i = 0; i < items.size(); i++) {
            itemsJson.append(items.get(i).toJson());
            if (i < items.size() - 1) {
                itemsJson.append(",");
            }
        }
        itemsJson.append("]");

        return "{"
                + "\"billId\":\"" + JsonUtils.escape(billId) + "\","
                + "\"patientId\":\"" + JsonUtils.escape(patientId) + "\","
                + "\"patientName\":\"" + JsonUtils.escape(patientName) + "\","
                + "\"items\":" + itemsJson.toString() + ","
                + "\"totalAmount\":" + totalAmount + ","
                + "\"isPaid\":" + isPaid + ","
                + "\"createdDate\":\"" + JsonUtils.escape(createdDate) + "\","
                + "\"notes\":\"" + JsonUtils.escape(notes) + "\""
                + "}";
    }

    /**
     * Deserializes a Bill object from a JSON string.
     *
     * @param json JSON input string
     * @return Bill instance
     */
    public static Bill fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        String billId = JsonUtils.getString(json, "billId");
        String patientId = JsonUtils.getString(json, "patientId");
        String patientName = JsonUtils.getString(json, "patientName");
        double totalAmount = JsonUtils.getDouble(json, "totalAmount");
        boolean isPaid = JsonUtils.getBoolean(json, "isPaid");
        String createdDate = JsonUtils.getString(json, "createdDate");
        String notes = JsonUtils.getString(json, "notes");

        List<BillItem> itemsList = new ArrayList<>();
        List<String> rawItems = JsonUtils.getArray(json, "items");
        for (String rawItem : rawItems) {
            BillItem item = BillItem.fromJson(rawItem);
            if (item != null) {
                itemsList.add(item);
            }
        }

        Bill bill = new Bill(billId, patientId, patientName, itemsList, totalAmount, isPaid, createdDate, notes);
        if (totalAmount <= 0.0 && !itemsList.isEmpty()) {
            bill.recalculateTotal();
        }
        return bill;
    }

    @Override
    public String toString() {
        return "Bill{" +
                "billId='" + billId + '\'' +
                ", patientName='" + patientName + '\'' +
                ", totalAmount=" + totalAmount +
                ", isPaid=" + isPaid +
                ", itemsCount=" + items.size() +
                '}';
    }
}
