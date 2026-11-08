package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.*;
import org.apache.poi.hpsf.Decimal;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tax_codes", schema = "itibari")
public class TaxCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.TaxCodeView.class, BaseView.ProductDetailView.class})
    @Column(name = "code")
    private Integer code;

    @JsonView({BaseView.TaxCodeView.class, BaseView.ProductDetailView.class})
    @Column(name = "code_name")
    private String codeName;

    @JsonView({BaseView.TaxCodeView.class, BaseView.ProductDetailView.class})
    @Column(name = "code_value")
    private BigDecimal codeValue = BigDecimal.ZERO;
}
