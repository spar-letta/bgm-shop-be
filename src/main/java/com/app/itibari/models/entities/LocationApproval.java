package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.LocationApprovalStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "location_approval", schema = "itibari")
public class LocationApproval extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.LocationApprovalView.class)
    @Column(name = "delivery_agent")
    private String deliveryAgent;

    @JsonView(BaseView.LocationApprovalView.class)
    @Column(name = "latitude")
    private String latitude;

    @JsonView(BaseView.LocationApprovalView.class)
    @Column(name = "longitude")
    private String longitude;

    @JsonView(BaseView.LocationApprovalView.class)
    @Column(name = "comment")
    private String comment;

    @JsonView(BaseView.LocationApprovalView.class)
    @Column(name = "customer_name")
    private String customerName;

    @JsonView(BaseView.LocationApprovalView.class)
    @Column(name = "customer_phone")
    private String customerPhone;

    @JsonView(BaseView.LocationApprovalView.class)
    @Column(name = "customer_public_id")
    private String customerPublicId;

    @JsonView(BaseView.LocationApprovalView.class)
    @Column(name = "route_public_id")
    private String routePublicId;

    @JsonView(BaseView.LocationApprovalView.class)
    @Column(name = "order_public_id")
    private String orderPublicId;

    @JsonView(BaseView.LocationApprovalView.class)
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private LocationApprovalStatus status;

    @JsonView(BaseView.LocationApprovalView.class)
    @Column(name = "previous_latitude")
    private String previousLatitude;

    @JsonView(BaseView.LocationApprovalView.class)
    @Column(name = "previous_longitude")
    private String previousLongitude;

    @JsonView(BaseView.LocationApprovalView.class)
    @Column(name = "comments")
    private String comments;

    @JsonView(BaseView.LocationApprovalView.class)
    @Column(name = "number_of_attempts")
    private Integer numberOfAttempts;

    @JsonView(BaseView.LocationApprovalView.class)
    @Column(name = "call_status")
    private String callStatus;




    public String getDistanceFromCustomer() {
//        if (latitude == null || longitude == null || previousLatitude == null || previousLongitude == null) {
//            return null;
//        }
//        try {
//            double lat1 = Double.parseDouble(latitude);
//            double lon1 = Double.parseDouble(longitude);
//            double lat2 = Double.parseDouble(previousLatitude);
//            double lon2 = Double.parseDouble(previousLongitude);
//            return String.format("%.2f m", haversine(lat1, lon1, lat2, lon2));
//        } catch (NumberFormatException e) {
//            return null;
//        }
        return null;
    }

//    private Integer haversine(double lat1, double lon1, double lat2, double lon2) {
//        final int R = 6371000; // Radius of the Earth in meters
//        double latDistance = Math.toRadians(lat2 - lat1);
//        double lonDistance = Math.toRadians(lon2 - lon1);
//        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
//                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
//                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
//        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//        return (int) (R * c); // Distance in meters
//
//    }
}
