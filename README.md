# Local Configuration

Sensitive values such as database credentials and API keys should not be committed.

Use environment variables for local development:

```powershell
$env:PADONG_DB_URL="jdbc:mysql://localhost:3306/padong"
$env:PADONG_DB_USERNAME="root"
$env:PADONG_DB_PASSWORD="your_db_password"
$env:SEOUL_OPEN_API_BASE_URL="http://openapi.seoul.go.kr:8088"
$env:SEOUL_OPEN_API_KEY="your_seoul_open_api_key"
./gradlew bootRun
```

You can use [.env.example](G:\파동\padong_server\.env.example) as a reference when setting up your local environment.
