-- Create roles for all services
DO $$
DECLARE
    svc_record RECORD;
BEGIN
    FOR svc_record IN SELECT unnest(ARRAY['identity','customer','kyc','account','ledger','transaction','payment','card','loan','risk','compliance','support']) AS svc LOOP
        -- Create role if it doesn't exist
        IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = svc_record.svc || '_svc') THEN
            EXECUTE format('CREATE ROLE %I_svc WITH LOGIN PASSWORD ''%s'';', svc_record.svc, 'changeMe!');
        END IF;
    END LOOP;
END $$;

-- Create databases (must be done outside of function)
CREATE DATABASE IF NOT EXISTS identity_service WITH OWNER identity_svc;
CREATE DATABASE IF NOT EXISTS customer_service WITH OWNER customer_svc;
CREATE DATABASE IF NOT EXISTS kyc_service WITH OWNER kyc_svc;
CREATE DATABASE IF NOT EXISTS account_service WITH OWNER account_svc;
CREATE DATABASE IF NOT EXISTS ledger_service WITH OWNER ledger_svc;
CREATE DATABASE IF NOT EXISTS transaction_service WITH OWNER transaction_svc;
CREATE DATABASE IF NOT EXISTS payment_service WITH OWNER payment_svc;
CREATE DATABASE IF NOT EXISTS card_service WITH OWNER card_svc;
CREATE DATABASE IF NOT EXISTS loan_service WITH OWNER loan_svc;
CREATE DATABASE IF NOT EXISTS risk_service WITH OWNER risk_svc;
CREATE DATABASE IF NOT EXISTS compliance_service WITH OWNER compliance_svc;
CREATE DATABASE IF NOT EXISTS support_service WITH OWNER support_svc;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE identity_service TO identity_svc;
GRANT ALL PRIVILEGES ON DATABASE customer_service TO customer_svc;
GRANT ALL PRIVILEGES ON DATABASE kyc_service TO kyc_svc;
GRANT ALL PRIVILEGES ON DATABASE account_service TO account_svc;
GRANT ALL PRIVILEGES ON DATABASE ledger_service TO ledger_svc;
GRANT ALL PRIVILEGES ON DATABASE transaction_service TO transaction_svc;
GRANT ALL PRIVILEGES ON DATABASE payment_service TO payment_svc;
GRANT ALL PRIVILEGES ON DATABASE card_service TO card_svc;
GRANT ALL PRIVILEGES ON DATABASE loan_service TO loan_svc;
GRANT ALL PRIVILEGES ON DATABASE risk_service TO risk_svc;
GRANT ALL PRIVILEGES ON DATABASE compliance_service TO compliance_svc;
GRANT ALL PRIVILEGES ON DATABASE support_service TO support_svc;
