import { useRef, useState } from 'react';
import AvatarPlaceholder from '../ui/AvatarPlaceholder';
import InputField from '../ui/InputField';
import PillButton from '../ui/PillButton';
import { useAuth } from '../../context/AuthContext';

function mapSignInError(status) {
  if (status === 401) return 'Invalid email or password.';
  if (status === 403) return 'Account not verified. Please check your email.';
  return 'Something went wrong. Please try again.';
}

export default function SignInForm() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [status, setStatus] = useState('idle');
  const [message, setMessage] = useState('');
  const submittingRef = useRef(false);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!email || !password) return;
    if (submittingRef.current) return;
    submittingRef.current = true;
    setStatus('loading');
    setMessage('');
    try {
      await login(email, password);
      setStatus('success');
      setMessage('Login successful!');
    } catch (err) {
      setStatus('error');
      setMessage(mapSignInError(err.status));
    } finally {
      submittingRef.current = false;
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6">
      <AvatarPlaceholder />
      <InputField
        label="Email"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <InputField
        label="Password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
      <PillButton type="submit" disabled={status === 'loading' || status === 'success'}>
        {status === 'loading' ? 'Loading...' : 'Continue'}
      </PillButton>
      {message && (
        <p className={`text-center text-sm font-medium ${
          status === 'success' ? 'text-green-600' : status === 'error' ? 'text-red-500' : ''
        }`}>
          {message}
        </p>
      )}
      <hr className="border-[#3A3A3A]" />
      <div className="text-center">
        <a href="#" className="text-sm text-gray-400 underline">
          Forgot Password?
        </a>
      </div>
    </form>
  );
}
