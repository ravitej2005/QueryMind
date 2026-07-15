import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { registerUser } from './authApi';
import { useAuthStore } from './useAuthStore';

export default function RegisterPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [form, setForm] = useState({ email: '', password: '', displayName: '' });

  const mutation = useMutation({
    mutationFn: registerUser,
    onSuccess: (data) => {
      setAuth(data);
      navigate('/ask');
    },
  });

  function update(field) {
    return (e) => setForm((f) => ({ ...f, [field]: e.target.value }));
  }

  function handleSubmit(e) {
    e.preventDefault();
    mutation.mutate(form);
  }

  return (
    <div className="flex h-screen items-center justify-center bg-bg-base">
      <form onSubmit={handleSubmit} className="w-full max-w-sm rounded-lg border border-border bg-bg-surface p-8">
        <h1 className="mb-6 text-xl font-semibold text-text-primary">Create your QueryMind account</h1>

        <label className="mb-1 block text-sm text-text-secondary" htmlFor="displayName">Name</label>
        <input
          id="displayName"
          value={form.displayName}
          onChange={update('displayName')}
          required
          className="mb-4 w-full rounded border border-border bg-bg-surface-raised px-3 py-2 text-text-primary outline-none focus:ring-2 focus:ring-accent"
        />

        <label className="mb-1 block text-sm text-text-secondary" htmlFor="email">Email</label>
        <input
          id="email"
          type="email"
          value={form.email}
          onChange={update('email')}
          required
          className="mb-4 w-full rounded border border-border bg-bg-surface-raised px-3 py-2 text-text-primary outline-none focus:ring-2 focus:ring-accent"
        />

        <label className="mb-1 block text-sm text-text-secondary" htmlFor="password">Password (min 8 chars)</label>
        <input
          id="password"
          type="password"
          minLength={8}
          value={form.password}
          onChange={update('password')}
          required
          className="mb-4 w-full rounded border border-border bg-bg-surface-raised px-3 py-2 text-text-primary outline-none focus:ring-2 focus:ring-accent"
        />

        {mutation.isError && <p className="mb-4 text-sm text-danger">{mutation.error.message}</p>}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full rounded bg-accent px-4 py-2 font-medium text-white transition-opacity hover:opacity-90 disabled:opacity-50"
        >
          {mutation.isPending ? 'Creating account…' : 'Create account'}
        </button>

        <p className="mt-4 text-center text-sm text-text-secondary">
          Already have an account? <Link to="/login" className="text-accent">Log in</Link>
        </p>
      </form>
    </div>
  );
}
