DO $$
DECLARE
    svc_record RECORD;
BEGIN
    FOR svc_record IN SELECT unnest(ARRAY['identity','customer','kyc','account','ledger','transaction','payment','card','loan','risk','compliance','support']) AS svc LOOP
        EXECUTE format('CREATE ROLE %I_svc WITH LOGIN PASSWORD ''%s'';', svc_record.svc, 'changeMe!')
        ON CONFLICT DO NOTHING;
        EXECUTE format('CREATE DATABASE %I_service WITH OWNER %I_svc;', svc_record.svc, svc_record.svc)
        ON CONFLICT DO NOTHING;
        EXECUTE format('GRANT ALL PRIVILEGES ON DATABASE %I_service TO %I_svc;', svc_record.svc, svc_record.svc);
    END LOOP;
END $$;
