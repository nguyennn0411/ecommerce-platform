# StepZone Order UI

React/Vite UI for the customer order screens:

- `My Orders`
- `Order Detail`

The app tries `GET /api/v1/orders` first and falls back to mock data matching the project report screenshots.

## Run on Windows PowerShell

PowerShell may block `npm.ps1`, so use `npm.cmd`:

```powershell
npm.cmd install
npm.cmd run dev
```

Open `http://localhost:5173`.

To point the UI directly at the backend instead of the Vite proxy:

```powershell
$env:VITE_ORDER_API_BASE = "http://localhost:8084"
npm.cmd run dev
```
