import {Route, Routes} from "react-router-dom";
import {NotFoundPage} from "./pages/NotFoundPage.tsx";
import {LoginPage} from "./pages/LoginPage.tsx";

export default function App() {

  return (
    <Routes>
        <Route path="/" element={<LoginPage />} />

        <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
