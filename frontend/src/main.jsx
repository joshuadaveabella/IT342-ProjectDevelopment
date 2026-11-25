import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './AuthContext';
import ProtectedRoute, { HomeRedirect } from './components/ProtectedRoute';
import Layout from './components/Layout';
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import Profile from './pages/Profile';
import { Forbidden, LeadDashboard, MemberDashboard, TeamAccounts } from './pages/Dashboard';
import './styles.css';

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<HomeRedirect />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/forbidden" element={<Forbidden />} />
          <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
            <Route path="/profile" element={<ProtectedRoute roles={['MEMBER', 'LEAD']}><Profile /></ProtectedRoute>} />
            <Route path="/member" element={<ProtectedRoute roles={['MEMBER', 'LEAD']}><MemberDashboard /></ProtectedRoute>} />
            <Route path="/lead" element={<ProtectedRoute roles={['LEAD']}><LeadDashboard /></ProtectedRoute>} />
            <Route path="/lead/team" element={<ProtectedRoute roles={['LEAD']}><TeamAccounts /></ProtectedRoute>} />
          </Route>
          <Route path="*" element={<HomeRedirect />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);
