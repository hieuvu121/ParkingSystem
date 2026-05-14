import { useRef, useState } from 'react';
import InputField from '../ui/InputField';
import PillButton from '../ui/PillButton';
import { useAuth } from '../../context/AuthContext';

function mapSignUpError(status, backendMessage) {
  if (status === 409) return 'This email is already registered.';
  if (status === 400) return backendMessage ?? 'Invalid input. Please check your details.';
  return 'Something went wrong. Please try again.';
}

export default function SignUpForm() {
  const { register } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [status, setStatus] = useState('idle');
  const [message, setMessage] = useState('');
  const submittingRef = useRef(false);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!name || !email || !password || !confirm) return;
    if (password !== confirm) {
      setStatus('error');
      setMessage('Passwords do not match.');
      return;
    }
    if (submittingRef.current) return;
    submittingRef.current = true;
    setStatus('loading');
    setMessage('');
    try {
      await register(name, email, password);
      setStatus('success');
      setMessage('Registration successful. Please check your email to verify your account.');
    } catch (err) {
      setStatus('error');
      setMessage(mapSignUpError(err.status, err.message));
    } finally {
      submittingRef.current = false;
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6">
      <InputField
        label="Name"
        value={name}
        onChange={(e) => setName(e.target.value)}
      />
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
      <InputField
        label="Confirm password"
        type="password"
        value={confirm}
        onChange={(e) => setConfirm(e.target.value)}
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
    </form>
  );
}
