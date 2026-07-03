-- Retail account demo content modules (minimal seed)
INSERT INTO content_module (id, code, name, group_code, structure_json, created_at, updated_at)
VALUES
  ('a1000001-0001-4000-8000-000000000001', 'RETAIL-STD-OPENING', 'Standard Opening Clause', 'RETAIL',
   '{"type":"paragraph","children":[{"type":"textRun","text":"Standard retail opening terms apply."}]}',
   NOW(), NOW())
ON CONFLICT (code) DO NOTHING;
