package io.ordermanagement.domain.model;

import java.util.Date;
import java.util.List;

public class DrinksOrdered implements DomainEvent {

    private Date occurredOn;
    private String tabId;
    private List<OrderItem> items;

    public DrinksOrdered(String tabId, List<OrderItem> items) {
        this.occurredOn = new Date();
        this.tabId = tabId;
        this.items = items;
    }

    @Override
    public Date occurredOn() {
        return this.occurredOn;
    }

    public String getTabId() {
        return tabId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DrinksOrdered that = (DrinksOrdered) o;

        if (tabId != null ? !tabId.equals(that.tabId) : that.tabId != null) return false;
        return items != null ? items.equals(that.items) : that.items == null;
    }

    @Override
    public int hashCode() {
        int result = tabId != null ? tabId.hashCode() : 0;
        result = 31 * result + (items != null ? items.hashCode() : 0);
        return result;
    }
}