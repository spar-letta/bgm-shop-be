package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "order_cut_off_time", schema = "itibari")
public class OrderCutOffTime extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.VehicleView.class)
    @Column(name = "cut_off_time")
    private LocalTime cutOffTime;

    @JsonView(BaseView.VehicleView.class)
    @Column(name = "cut_off_time_end")
    private LocalTime cutOffTimeEnd;

    @JsonView(BaseView.VehicleView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id_fk", referencedColumnName = "id")
    private Warehouse warehouse;
}
