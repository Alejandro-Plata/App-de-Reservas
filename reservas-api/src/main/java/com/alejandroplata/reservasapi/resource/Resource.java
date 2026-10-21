package com.alejandroplata.reservasapi.resource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceCategory category;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    protected Resource() {
    }

    public Resource(String name, String description, ResourceCategory category, int capacity) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.capacity = capacity;
    }

    public void update(String name, String description, ResourceCategory category, int capacity) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.capacity = capacity;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ResourceCategory getCategory() { return category; }
    public int getCapacity() { return capacity; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
