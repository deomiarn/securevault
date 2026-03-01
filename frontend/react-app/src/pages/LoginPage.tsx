import { type FormEvent, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export default function LoginPage() {
    const [ email, setEmail ] = useState('');
    const [ password, setPassword ] = useState('');
    const [ error, setError ] = useState<string | null>(null);
    const [ isLoading, setIsLoading ] = useState(false);

    const { login, isAuthenticated } = useAuth();
    const navigate = useNavigate();

    if (isAuthenticated) {
        return <Navigate to="/vault" replace />;
    }

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setError(null);
        setIsLoading(true);

        try {
            await login(email, password);
            navigate('/vault');
        } catch (err: unknown) {
            if (err instanceof Error) {
                setError(err.message);
            } else {
                setError('Login fehlgeschlagen');
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="flex min-h-svh items-center justify-center p-4">
            <Card className="w-full max-w-md">
                <CardHeader className="text-center">
                    <CardTitle className="text-2xl">SecureVault</CardTitle>
                    <CardDescription>Melde dich an, um auf deinen Vault zuzugreifen</CardDescription>
                </CardHeader>
                <CardContent>
                    <form onSubmit={ handleSubmit } className="space-y-4">
                        { error && (
                            <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
                                { error }
                            </div>
                        ) }
                        <div className="space-y-2">
                            <Label htmlFor="email">Email</Label>
                            <Input
                                id="email"
                                type="email"
                                placeholder="name@example.com"
                                value={ email }
                                onChange={ (e) => setEmail(e.target.value) }
                                required
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="password">Passwort</Label>
                            <Input
                                id="password"
                                type="password"
                                value={ password }
                                onChange={ (e) => setPassword(e.target.value) }
                                required
                            />
                        </div>
                        <Button type="submit" className="w-full" disabled={ isLoading }>
                            { isLoading ? 'Anmelden...' : 'Anmelden' }
                        </Button>
                    </form>
                    <div className="mt-4 text-center text-sm text-muted-foreground">
                        Kein Account?{ ' ' }
                        <Link to="/register" className="text-primary underline">
                            Registrieren
                        </Link>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
