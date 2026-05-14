import { useState } from 'react';
import InputField from '../ui/InputField';
import PillButton from '../ui/PillButton';

export default function SignUpForm() {
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');

  function handleSubmit(e) {
    e.preventDefault();
    if (!name || !phone || !email || !password || !confirm) return;
    if (password !== confirm) return;
    console.log('sign-up', { name, phone, email });
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6">
      <InputField
        label="Name"
        value={name}
        onChange={(e) => setName(e.target.value)}
      />
      <InputField
        label="Phone number"
        value={phone}
        onChange={(e) => setPhone(e.target.value)}
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
      <PillButton onClick={handleSubmit}>Continue</PillButton>
    </form>
  );
}
