INSERT INTO ${myuniversity}_${mymodule}.feefines (id, jsonb) VALUES
( '6b830703-f828-4e38-a0bb-ee81deacbd03',
  '{
   "id": "6b830703-f828-4e38-a0bb-ee81deacbd03",
   "feeFineType": "Reminder fee",
   "automatic": true
  }'
) ON CONFLICT DO NOTHING;
