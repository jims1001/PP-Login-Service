import { Routes, Route, Navigate } from "react-router-dom";
import AuthFlowPage from "./pages/AuthFlowPage";

export default function App() {
    return (
        <Routes>
            <Route path="/" element={<Navigate to="/login" replace />} />
            <Route path="/login" element={<AuthFlowPage flowType="LOGIN" />} />
            <Route path="/register" element={<AuthFlowPage flowType="REGISTER" />} />
            <Route path="/reset" element={<AuthFlowPage flowType="RESET_PASSWORD" />} />
        </Routes>
    );
}