package com.example.supermarket;

public class DebtItem {
    private String id;
    private String customerId; // معرف الزبون المرتبط بهذه الحركة للوصول السريع بدون إنترنت
    private String itemName;
    private double amount;
    private long timestamp;
    private long dueDate;

    public DebtItem() {}

    public DebtItem(String id, String itemName, double amount, long timestamp) {
        this.id = id;
        this.itemName = itemName;
        this.amount = amount;
        this.timestamp = timestamp;
        this.dueDate = 0;
    }

    public DebtItem(String id, String itemName, double amount, long timestamp, long dueDate) {
        this.id = id;
        this.itemName = itemName;
        this.amount = amount;
        this.timestamp = timestamp;
        this.dueDate = dueDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }
}