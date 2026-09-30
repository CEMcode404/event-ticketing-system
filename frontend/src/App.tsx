import { Routes, Route, useNavigate } from "react-router-dom";
import { Auth0Provider, type AppState } from "@auth0/auth0-react";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { DashboardPage } from "./pages/DashboardPage";
import { CreateEventPage } from "./pages/CreateEventPage";
import { EventDetailPage } from "./pages/EventDetailPage";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { QueuePage } from "./pages/QueuePage";
import { ShopPage } from "./pages/ShopPage";

function App() {
  const navigate = useNavigate();

  function onRedirectCallback(appState?: AppState) {
    navigate(appState?.returnTo ?? "/admin");
  }

  return (
    <Auth0Provider
      domain={import.meta.env.VITE_AUTH0_DOMAIN}
      clientId={import.meta.env.VITE_AUTH0_CLIENT_ID}
      authorizationParams={{
        redirect_uri: window.location.origin,
        audience: import.meta.env.VITE_AUTH0_AUDIENCE,
      }}
      onRedirectCallback={onRedirectCallback}
      useRefreshTokens={true}
      cacheLocation="localstorage"
    >
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/admin/login" element={<LoginPage />} />
        <Route path="/events/:id/queue" element={<QueuePage />} />
        <Route path="/events/:id/shop" element={<ShopPage />} />
        <Route
          path="/admin"
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/events/new"
          element={
            <ProtectedRoute>
              <CreateEventPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/events/:id"
          element={
            <ProtectedRoute>
              <EventDetailPage />
            </ProtectedRoute>
          }
        />
      </Routes>
    </Auth0Provider>
  );
}

export default App;