@echo off
set PGPASSWORD=123456
set PSQL="C:\Program Files\PostgreSQL\16\bin\psql.exe"

echo Creating database...
%PSQL% -U postgres -c "CREATE DATABASE collateral_db;" 2>&1

echo Creating tables...
%PSQL% -U postgres -d collateral_db -f "C:\Users\admin\Desktop\stablecoin_system\collateral-service\src\main\resources\db\init.sql" 2>&1

echo Done!
