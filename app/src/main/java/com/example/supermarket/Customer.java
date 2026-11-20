package com.example.supermarket;

public class Customer {
    private String id;
    private String name;
    private String phone;
    private double totalDebt;
    private long dueDate;
    private boolean archived = false;
    private long deletedAt = 0;

    // Constructor فارغ مطلوب لـ Firebase
    public Customer() {}

    public Customer(String id, String name, String phone, double totalDebt, long dueDate) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.totalDebt = totalDebt;
        this.dueDate = dueDate;
        this.archived = false;
        this.deletedAt = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public double getTotalDebt() { return totalDebt; }
    public void setTotalDebt(double totalDebt) { this.totalDebt = totalDebt; }

    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public long getDeletedAt() { return deletedAt; }
    public void setDeletedAt(long deletedAt) { this.deletedAt = deletedAt; }
}