package io.ordermanagement.domain.model;

import io.ordermanagement.application.OpenTab;

class Tab implements Aggregate {

    private DomainEventPublisher domainEventPublisher;

    Tab(DomainEventPublisher domainEventPublisher) {
        this.domainEventPublisher = domainEventPublisher;
    }

    void handle(OpenTab c) {
        domainEventPublisher.publish(new TabOpened(
                c.getId(),
                c.getTableNumber(),
                c.getWaiter())
        );
    }

}