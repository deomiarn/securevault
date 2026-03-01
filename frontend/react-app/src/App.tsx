import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from "@/pages/LoginPage.tsx";

function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <Routes>
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<div>Register (coming soon)</div>} />
                    <Route
                        path="/vault"
                        element={
                            <ProtectedRoute>
                                <div>Vault (coming soon)</div>
                            </ProtectedRoute>
                        }
                    />
                    <Route path="*" element={<Navigate to="/vault" replace />} />
                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}

export default App
