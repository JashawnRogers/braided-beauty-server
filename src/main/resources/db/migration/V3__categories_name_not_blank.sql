ALTER TABLE categories
  ADD CONSTRAINT categories_name_not_blank CHECK (btrim(name) <> '');