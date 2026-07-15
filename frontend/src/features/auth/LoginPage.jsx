import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { loginUser } from './authApi';
import { useAuthStore } from './useAuthStore';

export default function LoginPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const mutation = useMutation({
    mutationFn: loginUser,
    onSuccess: (data) => {
      setAuth(data);
      navigate('/ask');
    },
  });

  function handleSubmit(e) {
    e.preventDefault();
    mutation.mutate({ email, password });
  }

  return (
    <div className="flex h-screen items-center justify-center bg-bg-base">
      <form onSubmit={handleSubmit} className="w-full max-w-sm rounded-lg border border-border bg-bg-surface p-8">
        <h1 className="mb-6 text-xl font-semibold text-text-primary">Log in to QueryMind</h1>

        <label className="mb-1 block text-sm text-text-secondary" htmlFor="email">Email</label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          className="mb-4 w-full rounded border border-border bg-bg-surface-raised px-3 py-2 text-text-primary outline-none focus:ring-2 focus:ring-accent"
        />

        <label className="mb-1 block text-sm text-text-secondary" htmlFor="password">Password</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          className="mb-4 w-full rounded border border-border bg-bg-surface-raised px-3 py-2 text-text-primary outline-none focus:ring-2 focus:ring-accent"
        />

        {mutation.isError && (
          <p className="mb-4 text-sm text-danger">{mutation.error.message}</p>
        )}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full rounded bg-accent px-4 py-2 font-medium text-white transition-opacity hover:opacity-90 disabled:opacity-50"
        >
          {mutation.isPending ? 'Logging in…' : 'Log in'}
        </button>

        <p className="mt-4 text-center text-sm text-text-secondary">
          No account? <Link to="/register" className="text-accent">Register</Link>
        </p>
      </form>
    </div>
  );
}
