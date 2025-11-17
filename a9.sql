CREATE TABLE customer(
  customer_name  VARCHAR2(20) NOT NULL,
  customer_email VARCHAR2(40) NOT NULL,
  customer_phone VARCHAR2(12) NOT NULL,
  customer_id    INT NOT NULL,
  CONSTRAINT customer_pk PRIMARY KEY (customer_id)
);

CREATE TABLE menu (
  dish_id    INT NOT NULL,
  dish_name  VARCHAR2(30) NOT NULL,
  price      NUMBER(7,2) NOT NULL CHECK (price >= 0),
  quantity   NUMBER NOT NULL CHECK (quantity > 0), 
  category_id VARCHAR2(20) NOT NULL
              CHECK (category_id IN ('APPETIZER','MAIN','DESSERT','DRINK')),
  CONSTRAINT dishes_pk PRIMARY KEY (dish_id)
);

CREATE TABLE seating(
  table_id           INT NOT NULL,
  table_capacity     NUMBER(2) NOT NULL CHECK (table_capacity BETWEEN 1 AND 12),
  table_availability NUMBER(1) NOT NULL CHECK (table_availability IN (0,1)),
  CONSTRAINT seating_pk PRIMARY KEY (table_id)
);


CREATE TABLE bill_details (
  bill_id     INT NOT NULL,
  subtotal    NUMBER(10,2) NOT NULL,
  tax         NUMBER(10,2) NOT NULL,
  tip         NUMBER(10,2) DEFAULT 0 NOT NULL,
  split_count NUMBER(2) DEFAULT 1 NOT NULL,
  CONSTRAINT bill_details_pk PRIMARY KEY (bill_id)
);

CREATE TABLE bill_total (
  subtotal    NUMBER(10,2) NOT NULL,
  tax         NUMBER(10,2) NOT NULL,
  tip         NUMBER(10,2) NOT NULL,
  grand_total NUMBER(10,2) NOT NULL,
  CONSTRAINT bill_total_pk PRIMARY KEY (subtotal, tax, tip)
);

CREATE TABLE payment (
  payment_id     INT NOT NULL,
  bill_id        INT NOT NULL,
  payment_method VARCHAR2(5) NOT NULL CHECK (payment_method IN ('CARD','CASH')),
  amount         NUMBER(10,2) NOT NULL,
  CONSTRAINT payment_pk PRIMARY KEY (payment_id),
  CONSTRAINT fk_payment_bill FOREIGN KEY (bill_id)
    REFERENCES bill_details(bill_id)
);

CREATE TABLE reservations (
  reservation_id INT NOT NULL,
  customer_id    INT NOT NULL,
  bill_id        INT,             
  date_id        VARCHAR2(20),
  time_id        VARCHAR2(20),
  party_size     INT NOT NULL,
  table_id       INT NOT NULL,
  CONSTRAINT reservations_pk PRIMARY KEY (reservation_id),
  CONSTRAINT fk_reservations_customer FOREIGN KEY (customer_id)
    REFERENCES customer(customer_id),
  CONSTRAINT fk_reservations_bill FOREIGN KEY (bill_id)
    REFERENCES bill_details(bill_id),
  CONSTRAINT fk_seating FOREIGN KEY (table_id)
    REFERENCES seating(table_id)
);

CREATE TABLE online_order ( 
  online_order_id INT NOT NULL,
  customer_id     INT NOT NULL,
  bill_id         INT,
  pickup_time     VARCHAR2(20),
  order_status    NUMBER(1) NOT NULL CHECK (order_status IN (0,1)),
  dish_id         INT NOT NULL REFERENCES menu(dish_id),
  CONSTRAINT online_order_pk PRIMARY KEY (online_order_id),
  CONSTRAINT fk_online_order_customer FOREIGN KEY (customer_id)
    REFERENCES customer(customer_id),
  CONSTRAINT fk_online_order_bill FOREIGN KEY (bill_id)
    REFERENCES bill_details(bill_id)
);



-- CUSTOMERS
INSERT INTO customer VALUES ('Varen', 'varen@email.com',       '4192946250', 1);
INSERT INTO customer VALUES ('NJ',    'nithieshan@email.com',  '4167282882', 2); 
INSERT INTO customer VALUES ('Alwin', 'alwin@email.com',       '1234567890', 3); 
INSERT INTO customer VALUES ('Dave',  'dave@email.com',        '9050000000', 4);
INSERT INTO customer VALUES ('Sara',  'sara@email.com',        '4375551111', 5);
INSERT INTO customer VALUES ('Maya',  'maya@email.com',        '6472223333', 6);

-- MENU
INSERT INTO menu VALUES (1, 'Burger',    11.99, 10, 'MAIN');
INSERT INTO menu VALUES (2, 'Fries',      4.99, 20, 'APPETIZER');
INSERT INTO menu VALUES (3, 'Cake',       5.99, 5,  'DESSERT'); 
INSERT INTO menu VALUES (4, 'Soda',       2.50, 15, 'DRINK'); 
INSERT INTO menu VALUES (5, 'Steak',     22.99, 8,  'MAIN');
INSERT INTO menu VALUES (6, 'Lobster',   34.99, 6,  'MAIN');
INSERT INTO menu VALUES (7, 'Tiramisu',   8.99, 4,  'DESSERT');

-- SEATING
INSERT INTO seating VALUES (1, 2, 1); 
INSERT INTO seating VALUES (2, 4, 1); 
INSERT INTO seating VALUES (3, 6, 0); 
INSERT INTO seating VALUES (4, 2, 1);
INSERT INTO seating VALUES (5, 8, 1);

-- BILL_DETAILS + BILL_TOTAL (BCNF)
INSERT INTO bill_details VALUES (1, 45.50,  5.92, 7.00, 2);
INSERT INTO bill_total   VALUES (45.50,  5.92, 7.00, 67.60);

INSERT INTO bill_details VALUES (2, 22.00,  2.86, 3.00, 1);
INSERT INTO bill_total   VALUES (22.00,  2.86, 3.00, 27.86);

INSERT INTO bill_details VALUES (3, 78.25, 10.17, 8.00, 3);
INSERT INTO bill_total   VALUES (78.25, 10.17, 8.00, 96.42);

INSERT INTO bill_details VALUES (4, 18.99,  2.47, 0.00, 1);
INSERT INTO bill_total   VALUES (18.99,  2.47, 0.00, 21.46);

INSERT INTO bill_details VALUES (5, 55.00,  7.15, 5.00, 2);
INSERT INTO bill_total   VALUES (55.00,  7.15, 5.00, 67.15);

-- PAYMENT
INSERT INTO payment VALUES (1, 1, 'CARD', 67.60);
INSERT INTO payment VALUES (2, 2, 'CARD', 27.86); 
INSERT INTO payment VALUES (3, 3, 'CARD', 96.42);
INSERT INTO payment VALUES (4, 4, 'CASH', 21.46);
INSERT INTO payment VALUES (5, 5, 'CARD', 67.15);

-- RESERVATIONS
INSERT INTO reservations VALUES (1, 1, 1, '2025-09-15', '18:30', 2, 1);
INSERT INTO reservations VALUES (2, 2, 2, '2025-09-15', '20:00', 4, 2);
INSERT INTO reservations VALUES (3, 3, 3, '2025-09-16', '19:00', 3, 3);
INSERT INTO reservations VALUES (4, 4, 1, '2025-09-17', '18:00', 2, 1);  
INSERT INTO reservations VALUES (5, 6, 5, '2025-09-18', '17:00', 2, 4);  

-- ONLINE_ORDERS
INSERT INTO online_order VALUES (1, 1, 1, '08:45', 1, 1);   
INSERT INTO online_order VALUES (2, 2, 2, '09:15', 0, 2);   
INSERT INTO online_order VALUES (3, 3, 3, '10:00', 1, 3);   
INSERT INTO online_order VALUES (4, 5, 4, '11:15', 1, 5);  
INSERT INTO online_order VALUES (5, 5, 4, '12:45', 0, 1);   

COMMIT;


-- VIEWS 

CREATE OR REPLACE VIEW menu_summary_view AS 
SELECT category_id, COUNT(*) AS num_dishes
FROM menu 
GROUP BY category_id;

CREATE OR REPLACE VIEW reservation_detail_view AS 
SELECT
    r.reservation_id,
    c.customer_name,
    r.date_id,
    r.time_id,
    r.party_size,
    s.table_capacity,
    s.table_availability
FROM reservations r
JOIN customer c ON r.customer_id = c.customer_id
JOIN seating  s ON r.table_id    = s.table_id;

CREATE OR REPLACE VIEW online_order_view AS 
SELECT
    o.online_order_id,
    c.customer_name,
    m.dish_name,
    o.pickup_time, 
    CASE 
        WHEN o.order_status = 0 THEN 'PENDING' 
        WHEN o.order_status = 1 THEN 'READY' 
    END AS order_status 
FROM online_order o 
JOIN customer c ON o.customer_id = c.customer_id 
JOIN menu     m ON o.dish_id     = m.dish_id; 


-- ADVANCED QUERIES (JOIN / SET / GROUP BY)


SELECT c.customer_id, c.customer_name, c.customer_email, c.customer_phone
FROM   customer c
WHERE  EXISTS (SELECT 1 FROM reservations r
               WHERE r.customer_id = c.customer_id)
  AND  EXISTS (SELECT 1 FROM online_order o
               WHERE o.customer_id = c.customer_id)
ORDER BY c.customer_name;


WITH only_res AS (
  SELECT r.customer_id FROM reservations r
  MINUS
  SELECT o.customer_id FROM online_order o
)
SELECT c.customer_id, c.customer_name, c.customer_email
FROM   customer c
JOIN   only_res x ON x.customer_id = c.customer_id
ORDER BY c.customer_name;


SELECT 
    c.customer_id,
    c.customer_name,
    c.customer_email,
    c.customer_phone
FROM customer c
WHERE c.customer_id IN (
        SELECT customer_id FROM reservations
        UNION
        SELECT customer_id FROM online_order
)
ORDER BY c.customer_id;


WITH max_price AS (
  SELECT category_id, MAX(price) AS max_price
  FROM   menu
  GROUP  BY category_id
)
SELECT m.category_id, m.dish_id, m.dish_name, m.price
FROM   menu m
JOIN   max_price x
  ON   x.category_id = m.category_id
 AND   x.max_price   = m.price
ORDER  BY m.category_id, m.dish_name;

SELECT 
    d.split_count, 
    AVG(t.grand_total) AS avg_total
FROM 
    bill_details d
JOIN 
    bill_total t
ON  d.subtotal = t.subtotal
AND d.tax      = t.tax
AND d.tip      = t.tip
GROUP BY 
    d.split_count
HAVING 
    AVG(t.grand_total) > 0
ORDER BY 
    d.split_count;

SELECT
    c.customer_id,
    c.customer_name,
    c.customer_email,
    r.reservation_id,
    r.date_id,
    r.time_id,
    r.party_size,
    s.table_id,
    s.table_capacity,
    d.bill_id,
    d.subtotal,
    d.tax,
    d.tip,
    d.split_count,
    t.grand_total
FROM reservations   r
JOIN customer       c ON r.customer_id = c.customer_id
JOIN seating        s ON r.table_id    = s.table_id
JOIN bill_details   d ON r.bill_id     = d.bill_id
JOIN bill_total     t ON d.subtotal    = t.subtotal
                      AND d.tax        = t.tax
                      AND d.tip        = t.tip
ORDER BY c.customer_id, r.reservation_id;

SELECT *
FROM customer
WHERE customer_id = 1;

