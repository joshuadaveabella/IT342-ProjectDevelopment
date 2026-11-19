import axios from 'axios';

// Same-origin requests (Vite proxy). Cookies (session + XSRF-TOKEN) are sent automatically;
// axios copies the XSRF-TOKEN cookie into the X-XSRF-TOKEN header on every write request.
const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
});

export const ensureCsrf = () => api.get('/auth/csrf');

export function errorMessage(err, fallback = 'Something went wrong. Please try again.') {
  if (!err.response) return 'Cannot reach the server. Is the backend running?';
  return err.response.data?.error || fallback;
}
export const fieldErrors = (err) => err.response?.data?.fields || {};

export default api;
