import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <Routes>
                    <Route path="/login" element={<div>Login (coming soon)</div>} />
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
