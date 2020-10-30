INSERT INTO ${myuniversity}_${mymodule}.feefines (id, jsonb) VALUES
( '73785370-d3bd-4d92-942d-ae2268e02ded',
  '{
   "id": "73785370-d3bd-4d92-942d-ae2268e02ded",
   "feeFineType": "Lost item fee (actual cost)",
   "automatic": true
  }'
) ON CONFLICT DO NOTHING;
