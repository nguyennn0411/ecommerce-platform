-- ======================================================================
-- 1. CONNECT & INIT ECOMMERCE USER DB
-- ======================================================================
\c ecommerce_user_service;

CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(50) NOT NULL UNIQUE,
                       description TEXT
);

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role_id BIGINT NOT NULL,
                       status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT
);

CREATE TABLE user_profiles (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL UNIQUE,
                               full_name VARCHAR(100),
                               phone VARCHAR(20),
                               avatar_url VARCHAR(512),
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE addresses (
                           id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT NOT NULL,
                           receiver_name VARCHAR(100) NOT NULL,
                           receiver_phone VARCHAR(20) NOT NULL,
                           address_line VARCHAR(255) NOT NULL,
                           city VARCHAR(100) NOT NULL,
                           district VARCHAR(100) NOT NULL,
                           ward VARCHAR(100) NOT NULL,
                           is_default BOOLEAN NOT NULL DEFAULT FALSE,
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Chèn trước dữ liệu Role cơ bản để hệ thống không bị lỗi trống phân quyền
INSERT INTO roles (name, description) VALUES
                                          ('ROLE_ADMIN', 'Administrator of the system'),
                                          ('ROLE_USER', 'Standard registered customer')
    ON CONFLICT (name) DO NOTHING;


-- ======================================================================
-- 2. CONNECT & INIT ECOMMERCE PRODUCT DB (Đã sửa lại tên DB chuẩn khớp file 01)
-- ======================================================================
\c ecommerce_product_catalog_service;

CREATE TABLE categories (
                            id UUID PRIMARY KEY,
                            name VARCHAR(100) NOT NULL,
                            description TEXT,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP
);

CREATE TABLE products (
                          id UUID PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          brand VARCHAR(100),
                          description TEXT,
                          category_id UUID,
                          base_price NUMERIC(15, 2) NOT NULL,
                          status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP,

                          CONSTRAINT fk_products_category
                              FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE product_images (
                                id UUID PRIMARY KEY,
                                product_id UUID NOT NULL,
                                image_url TEXT NOT NULL,
                                is_main BOOLEAN DEFAULT FALSE,
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_product_images_product
                                    FOREIGN KEY (product_id) REFERENCES products(id)
);


-- ======================================================================
-- 3. CONNECT & INIT ECOMMERCE INVENTORY DB
-- ======================================================================
\c ecommerce_inventory_service;

CREATE TABLE inventory_items (
                                 id UUID PRIMARY KEY,
                                 product_id UUID NOT NULL,
                                 size VARCHAR(20) NOT NULL,
                                 color VARCHAR(50),
                                 quantity INT NOT NULL DEFAULT 0,
                                 reserved_quantity INT NOT NULL DEFAULT 0,
                                 status VARCHAR(50) NOT NULL DEFAULT 'IN_STOCK',
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP
);

CREATE TABLE stock_reservations (
                                    id UUID PRIMARY KEY,
                                    order_id UUID NOT NULL,
                                    product_id UUID NOT NULL,
                                    inventory_item_id UUID NOT NULL,
                                    quantity INT NOT NULL,
                                    status VARCHAR(50) NOT NULL DEFAULT 'RESERVED',
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP,

                                    CONSTRAINT fk_stock_reservations_inventory
                                        FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id)
);


-- ======================================================================
-- 4. CONNECT & INIT ECOMMERCE ORDER DB
-- ======================================================================
\c ecommerce_order_service;

CREATE TABLE orders (
                        id UUID PRIMARY KEY,
                        order_code VARCHAR(50) NOT NULL UNIQUE,
                        user_id UUID NOT NULL,
                        total_amount NUMERIC(15, 2) NOT NULL,
                        currency VARCHAR(10) NOT NULL DEFAULT 'VND',
                        status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                        shipping_address TEXT NOT NULL,
                        payment_id UUID,
                        note TEXT,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP
);

CREATE TABLE order_items (
                             id UUID PRIMARY KEY,
                             order_id UUID NOT NULL,
                             product_id UUID NOT NULL,
                             product_name VARCHAR(255) NOT NULL,
                             unit_price NUMERIC(15, 2) NOT NULL,
                             quantity INT NOT NULL,
                             subtotal NUMERIC(15, 2) NOT NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders(id);

CREATE TABLE saga_transactions (
                                   id UUID PRIMARY KEY,
                                   order_id UUID NOT NULL,
                                   saga_type VARCHAR(100) NOT NULL DEFAULT 'CREATE_ORDER',
                                   current_step VARCHAR(100),
                                   status VARCHAR(50) NOT NULL DEFAULT 'STARTED',
                                   error_message TEXT,
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP
);

ALTER TABLE saga_transactions
    ADD CONSTRAINT fk_saga_transactions_order
        FOREIGN KEY (order_id) REFERENCES orders(id);

CREATE TABLE saga_steps (
                            id UUID PRIMARY KEY,
                            saga_id UUID NOT NULL,
                            step_name VARCHAR(100) NOT NULL,
                            status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                            request_payload TEXT,
                            response_payload TEXT,
                            error_message TEXT,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP
);

ALTER TABLE saga_steps
    ADD CONSTRAINT fk_saga_steps_saga
        FOREIGN KEY (saga_id) REFERENCES saga_transactions(id);


-- ======================================================================
-- 5. CONNECT & INIT ECOMMERCE PAYMENT DB
-- ======================================================================
\c ecommerce_payment_service;

CREATE TABLE payments (
                          id UUID PRIMARY KEY,
                          order_id UUID NOT NULL,
                          user_id UUID NOT NULL,
                          amount NUMERIC(15, 2) NOT NULL,
                          currency VARCHAR(10) NOT NULL DEFAULT 'VND',
                          payment_method VARCHAR(50) NOT NULL,
                          status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                          provider VARCHAR(50),
                          provider_transaction_id VARCHAR(255),
                          failure_reason TEXT,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP
);

CREATE TABLE payment_transactions (
                                      id UUID PRIMARY KEY,
                                      payment_id UUID NOT NULL,
                                      transaction_type VARCHAR(50) NOT NULL,
                                      amount NUMERIC(15, 2) NOT NULL,
                                      status VARCHAR(50) NOT NULL,
                                      provider_response TEXT,
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT fk_payment_transactions_payment
                                          FOREIGN KEY (payment_id) REFERENCES payments(id)
);


-- ======================================================================
-- 6. CONNECT & INIT ECOMMERCE NOTIFICATION DB
-- ======================================================================
\c ecommerce_notification_service;

CREATE TABLE notifications (
                               id UUID PRIMARY KEY,
                               user_id UUID,
                               order_id UUID,
                               channel VARCHAR(30) NOT NULL,
                               recipient VARCHAR(255) NOT NULL,
                               subject VARCHAR(255),
                               content TEXT NOT NULL,
                               status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                               error_message TEXT,
                               sent_at TIMESTAMP,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP
);

CREATE TABLE notification_templates (
                                        id UUID PRIMARY KEY,
                                        template_code VARCHAR(100) NOT NULL UNIQUE,
                                        channel VARCHAR(30) NOT NULL,
                                        subject_template VARCHAR(255),
                                        body_template TEXT NOT NULL,
                                        active BOOLEAN DEFAULT TRUE,
                                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP
);