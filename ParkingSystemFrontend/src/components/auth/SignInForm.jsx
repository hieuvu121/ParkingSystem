import { useState } from 'react';
import AvatarPlaceholder from '../ui/AvatarPlaceholder';
import InputField from '../ui/InputField';
import PillButton from '../ui/PillButton';

export default function SignInForm() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  function handleSubmit(e) {
    e.preventDefault();
    if (!username || !password) return;
    console.log('sign-in', { username });
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6">
      <AvatarPlaceholder />
      <InputField
        label="Username"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
      />
      <InputField
        label="Password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
      <PillButton onClick={handleSubmit}>Continue</PillButton>
      <hr className="border-[#3A3A3A]" />
      <div className="text-center">
        <a href="#" className="text-sm text-gray-400 underline">
          Forgot Password?
        </a>
      </div>
    </form>
  );
}
