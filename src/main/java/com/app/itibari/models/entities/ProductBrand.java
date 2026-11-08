package com.app.itibari.models.entities;

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
@Table(name = "product_brands", schema = "itibari")
public class ProductBrand extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.ProductBrandView.class, BaseView.ProductDetailView.class})
    @Column(name = "name")
    private String name;

    @JsonView(BaseView.ProductBrandView.class)
    public String getBrandCreatedBy(){
        if (this.getCreatedBy() != null){
            return this.getCreatedBy().getFullName();
        }else {
            return "--";
        }
    }
}
