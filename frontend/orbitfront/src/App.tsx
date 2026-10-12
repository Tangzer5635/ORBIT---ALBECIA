import {Navigate, Route, Routes} from "react-router-dom";
import {NotFoundPage} from "./pages/NotFoundPage.tsx";
import {LoginPage} from "./pages/LoginPage.tsx";
import {PrivateRoute} from "./components/PrivateRoute.tsx";
import {HomePage} from "./pages/HomePage.tsx";

export default function App() {

  return (
      <Routes>
          <Route path="/connexion" element={<LoginPage />} />

          {/* Routes accessibles seulement une fois connecté */}
          <Route element={<PrivateRoute />}>
              <Route path="/accueil" element={<HomePage />} />
          </Route>

          <Route path="/" element={<Navigate to="/accueil" replace />} />
          <Route path="*" element={<NotFoundPage />} />
      </Routes>
  )
}
