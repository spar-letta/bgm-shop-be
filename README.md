# Project Documentation

## Code Standards

- Follow clean code principles
- Use meaningful variable and method names
- Implement proper documentation for all classes and methods
- Follow Jakarta EE and Spring best practices

## Testing Requirements

- Unit tests are mandatory for all business logic
- Integration tests for REST endpoints
- Minimum 80% code coverage required

## Database Management

- Liquibase is used for database version control
- All database changes must be documented in changelog files
- Follow proper migration naming conventions

## DTO Pattern Example

## Run on Metabase
## sample sql - count orders per supplier
SELECT DISTINCT so.*
FROM itibari.sales_orders so
JOIN itibari.customers c
ON so.sales_order_customer_fk = c.id
JOIN itibari.organization_regions org
ON c.customer_organization_region_fk = org.id
JOIN itibari.sales_order_items soi
ON so.id = soi.sales_order_id
JOIN itibari.products p
ON soi.sales_order_items_product_fk = p.id
JOIN itibari.product_suppliers ps
ON p.id = ps.product_id
JOIN itibari.suppliers s
ON ps.supplier_id = s.id
WHERE s.public_id = 'aadd4730-8200-4f79-a1d8-e9cedcdc5ff6'
AND so.loading_status = 'DELIVERED'
AND so. creation_time BETWEEN '2026-02-08' AND '2026-03-08'
AND org.public_id = '8c5d4db0-f9a1-47ed-a160-56780e4e4d14';

## Active customers
SELECT DISTINCT c.*
FROM itibari.sales_orders so
JOIN itibari.customers c
ON so.sales_order_customer_fk = c.id
JOIN itibari.organization_regions org
ON c.customer_organization_region_fk = org.id
JOIN itibari.sales_order_items soi
ON so.id = soi.sales_order_id
JOIN itibari.products p
ON soi.sales_order_items_product_fk = p.id
JOIN itibari.product_suppliers ps
ON p.id = ps.product_id
JOIN itibari.suppliers s
ON ps.supplier_id = s.id
WHERE s.public_id = 'aadd4730-8200-4f79-a1d8-e9cedcdc5ff6'
AND so.loading_status IN ('DELIVERED', 'ORDER_PLACED', 'IN_TRANSIT')
AND so. creation_time BETWEEN '2026-02-08' AND '2026-03-08'
AND c.activated = true
AND org.public_id = '8c5d4db0-f9a1-47ed-a160-56780e4e4d14';


## Supplier - quantity sold
SELECT COALESCE(SUM(p.weight * soi.quantity_delivered), 0) AS total_quantity_sold
FROM itibari.sales_orders so
JOIN itibari.customers c
ON so.sales_order_customer_fk = c.id
JOIN itibari.organization_regions org
ON c.customer_organization_region_fk = org.id
JOIN itibari.sales_order_items soi
ON so.id = soi.sales_order_id
JOIN itibari.products p
ON soi.sales_order_items_product_fk = p.id
JOIN itibari.product_suppliers ps
ON p.id = ps.product_id
JOIN itibari.suppliers s
ON ps.supplier_id = s.id
WHERE s.public_id = 'aadd4730-8200-4f79-a1d8-e9cedcdc5ff6'
AND so.loading_status IN ('DELIVERED', 'ORDER_PLACED', 'IN_TRANSIT')
AND so. creation_time BETWEEN '2026-02-08' AND '2026-03-08'
AND c.activated = true
AND org.public_id = '8c5d4db0-f9a1-47ed-a160-56780e4e4d14';

## Supplier - Cost price
SELECT COALESCE(SUM(p.buying_price * soi.quantity_delivered), 0) AS total_cost_price
FROM itibari.sales_orders so
JOIN itibari.customers c
ON so.sales_order_customer_fk = c.id
JOIN itibari.organization_regions org
ON c.customer_organization_region_fk = org.id
JOIN itibari.sales_order_items soi
ON so.id = soi.sales_order_id
JOIN itibari.products p
ON soi.sales_order_items_product_fk = p.id
JOIN itibari.product_suppliers ps
ON p.id = ps.product_id
JOIN itibari.suppliers s
ON ps.supplier_id = s.id
WHERE s.public_id = 'aadd4730-8200-4f79-a1d8-e9cedcdc5ff6'
AND so.loading_status IN ('DELIVERED', 'ORDER_PLACED', 'IN_TRANSIT')
AND so. creation_time BETWEEN '2026-02-08' AND '2026-03-08'
AND c.activated = true
AND org.public_id = '8c5d4db0-f9a1-47ed-a160-56780e4e4d14';

## 
## -- Main query: product distribution report
SELECT
p.product_name AS productName,
p.sku AS sku,
p.unit_of_measure AS uom,
SUM(p.buying_price * soi.quantity_delivered) AS orderValue,
SUM(soi.quantity_delivered) AS quantity_delivered
FROM itibari.sales_orders so
JOIN itibari.sales_order_items soi
ON so.id = soi.sales_order_id
JOIN itibari.products p
ON soi.sales_order_items_product_fk = p.id
JOIN itibari.product_suppliers ps
ON p.id = ps.product_id
JOIN itibari.suppliers s
ON ps.supplier_id = s.id
JOIN itibari.customers c
ON so.sales_order_customer_fk = c.id
WHERE so.loading_status = 'DELIVERED'
AND s.public_id = 'aadd4730-8200-4f79-a1d8-e9cedcdc5ff6'
AND so.creation_time BETWEEN '2026-02-08' AND '2026-03-08'
GROUP BY p.product_name, p.sku, p.unit_of_measure
ORDER BY p.product_name;
### Entity

