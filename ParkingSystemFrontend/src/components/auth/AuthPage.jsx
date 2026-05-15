import { useState } from 'react';
import AuthTabs from './AuthTabs';
import SignInForm from './SignInForm';
import SignUpForm from './SignUpForm';

export default function AuthPage() {
  const [activeTab, setActiveTab] = useState('signin');

  return (
    <div className="flex min-h-screen items-start justify-center bg-[#F8F7F0] pt-16">
      <div className="w-full max-w-md rounded-3xl bg-[#F8F7F0] p-10">
        <AuthTabs activeTab={activeTab} onTabChange={setActiveTab} />
        <div className="mt-10">
          {activeTab === 'signin' ? <SignInForm /> : <SignUpForm />}
        </div>
      </div>
    </div>
  );
}
