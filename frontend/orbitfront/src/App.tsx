import { Navigate, Route, Routes } from "react-router-dom";
import { NotFoundPage } from "./pages/NotFoundPage.tsx";
import { LoginPage } from "./pages/LoginPage.tsx";
import { HomePage } from "./pages/HomePage.tsx";
import { PrivateRoute } from "./components/PrivateRoute.tsx";
import { Layout } from "./components/Layout.tsx";
import {BatimentsPage} from "./pages/BatimentsPage.tsx";

export default function App() {
    return (
        <Routes>
            <Route path="/connexion" element={<LoginPage />} />

            {/* Connecté obligatoire, avec la Navbar */}
            <Route element={<PrivateRoute />}>
                <Route element={<Layout />}>
                    <Route path="/accueil" element={<HomePage />} />

                    {/* lien du gestionnaire */}
                    <Route element={<PrivateRoute roles={["GESTIONNAIRE"]} />}>
                         <Route path="/batiments" element={<BatimentsPage />} />
                    </Route>
                </Route>
            </Route>

            <Route path="/" element={<Navigate to="/accueil" replace />} />
            <Route path="*" element={<NotFoundPage />} />
        </Routes>
    );
}