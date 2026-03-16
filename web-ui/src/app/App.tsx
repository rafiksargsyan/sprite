import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { ProtectedRoute } from '../components/ProtectedRoute/ProtectedRoute';
import { EmailConfirmation } from '../components/EmailConfirmation/EmailConfirmation';
import { Layout } from '../components/Layout/Layout';
import { Login } from '../pages/Login';
import { Dashboard } from '../pages/Dashboard';
import { JobSpecs } from '../pages/JobSpecs';
import { Jobs } from '../pages/Jobs';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <EmailConfirmation />
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/job-specs" element={<JobSpecs />} />
            <Route path="/jobs" element={<Jobs />} />
            <Route path="/api-keys" element={<ComingSoon title="API Keys" />} />
            <Route path="/settings" element={<ComingSoon title="Settings" />} />
          </Route>
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

function ComingSoon({ title }: { title: string }) {
  return (
    <div>
      <h2>{title}</h2>
      <p style={{ color: '#666' }}>Coming soon.</p>
    </div>
  );
}
