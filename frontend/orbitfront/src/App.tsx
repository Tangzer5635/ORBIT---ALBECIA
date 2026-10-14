import { Route, Routes } from "react-router-dom";
import type { ReactElement } from "react";
import { NotFoundPage } from "./pages/NotFoundPage.tsx";
import { LoginPage } from "./pages/LoginPage.tsx";
import {PrivateRoute, RedirectionAccueil} from "./components/PrivateRoute.tsx";
import { Layout } from "./components/Layout.tsx";
import { BatimentsPage } from "./pages/BatimentsPage.tsx";
import SallePage from "./pages/SallePage.tsx";
import SalleDetailPage from "./pages/SalleDetailPage.tsx";
import DefaillancePage from "./pages/DefaillancePage.tsx";
import GarantiePage from "./pages/GarantiePage.tsx";
import MaterielPage from "./pages/MaterielPage.tsx";
import MasterPage from "./pages/MasterPage.tsx";
import StagePage from "./pages/StagePage.tsx";
import ProcessusPage from "./pages/ProcessusPage.tsx";
import UserPage from "./pages/UtilisateurPage.tsx";
import JournalPage from "./pages/JournalPage.tsx";
import PostePage from "./pages/PostePage.tsx";
import { ACCES } from "./config/acces.ts";

const PAGES: Record<string, ReactElement> = {
    "/batiments": <BatimentsPage />,
    "/salles": <SallePage />,
    "/salles/:salleId": <SalleDetailPage />,
    "/defaillances": <DefaillancePage />,
    "/garanties": <GarantiePage />,
    "/materiel": <MaterielPage />,
    "/masters": <MasterPage />,
    "/stages": <StagePage />,
    "/processus": <ProcessusPage />,
    "/poste": <PostePage />,
    "/utilisateurs": <UserPage />,
    "/journal": <JournalPage />,
};

export default function App() {
    return (
        <Routes>
            <Route path="/connexion" element={<LoginPage />} />

            {/* Redirige vers la page du rôle (ou vers la connexion) */}
            <Route path="/" element={<RedirectionAccueil />} />
            <Route path="/accueil" element={<RedirectionAccueil />} />

            <Route element={<PrivateRoute />}>
                <Route element={<Layout />}>
                    {ACCES.map(({ path, roles }) => (
                        <Route key={path} element={<PrivateRoute roles={roles} />}>
                            <Route path={path} element={PAGES[path]} />
                        </Route>
                    ))}
                </Route>
            </Route>

            <Route path="*" element={<NotFoundPage />} />
        </Routes>
    );
}