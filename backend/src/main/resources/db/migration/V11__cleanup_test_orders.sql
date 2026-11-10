-- Cleanup known test/pending orders (safe, idempotent). Keeps paid/real orders intact.
DELETE FROM webhook_events
WHERE order_id IN (SELECT id FROM orders WHERE order_number IN ('FT-479776','FT-799468','FT-614685','FT-276950','FT-357318','FT-976390','FT-464131','FT-943210'));

DELETE FROM shipments
WHERE order_id IN (SELECT id FROM orders WHERE order_number IN ('FT-479776','FT-799468','FT-614685','FT-276950','FT-357318','FT-976390','FT-464131','FT-943210'));

DELETE FROM payments
WHERE order_id IN (SELECT id FROM orders WHERE order_number IN ('FT-479776','FT-799468','FT-614685','FT-276950','FT-357318','FT-976390','FT-464131','FT-943210'));

DELETE FROM order_items
WHERE order_id IN (SELECT id FROM orders WHERE order_number IN ('FT-479776','FT-799468','FT-614685','FT-276950','FT-357318','FT-976390','FT-464131','FT-943210'));

DELETE FROM orders
WHERE order_number IN ('FT-479776','FT-799468','FT-614685','FT-276950','FT-357318','FT-976390','FT-464131','FT-943210');
