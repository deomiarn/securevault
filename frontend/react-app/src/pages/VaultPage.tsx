import { useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { vaultApi } from '@/api/vault';
import { Button } from '@/components/ui/button';
import SecretCard from '@/components/SecretCard';
import SecretModal from '@/components/SecretModal';
import type { SecretSummaryResponse, SecretResponse, SecretType } from '@/types/vault';

export default function VaultPage() {
    const { user, logout } = useAuth();
    const [secrets, setSecrets] = useState<SecretSummaryResponse[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [modalOpen, setModalOpen] = useState(false);
    const [editingSecret, setEditingSecret] = useState<SecretResponse | null>(null);

    const loadSecrets = async () => {
        setIsLoading(true);
        setError(null);
        try {
            const response = await vaultApi.getSecrets();
            setSecrets(response.content);
        } catch {
            setError('Secrets konnten nicht geladen werden');
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadSecrets();
    }, []);

    const handleCreate = () => {
        setEditingSecret(null);
        setModalOpen(true);
    };

    const handleView = async (id: string) => {
        try {
            const secret = await vaultApi.getSecret(id);
            setEditingSecret(secret);
            setModalOpen(true);
        } catch {
            setError('Secret konnte nicht geladen werden');
        }
    };

    const handleEdit = async (id: string) => {
        try {
            const secret = await vaultApi.getSecret(id);
            setEditingSecret(secret);
            setModalOpen(true);
        } catch {
            setError('Secret konnte nicht geladen werden');
        }
    };

    const handleSave = async (data: {
        name: string;
        value: string;
        description: string;
        secretType: SecretType;
    }) => {
        try {
            if (editingSecret) {
                await vaultApi.updateSecret(editingSecret.id, data);
            } else {
                await vaultApi.createSecret(data);
            }
            setModalOpen(false);
            setEditingSecret(null);
            await loadSecrets();
        } catch {
            setError('Speichern fehlgeschlagen');
        }
    };

    const handleDelete = async (id: string) => {
        try {
            await vaultApi.deleteSecret(id);
            await loadSecrets();
        } catch {
            setError('Loeschen fehlgeschlagen');
        }
    };

    return (
        <div className="min-h-svh bg-background">
            <header className="border-b">
                <div className="mx-auto flex max-w-5xl items-center justify-between p-4">
                    <h1 className="text-xl font-bold">SecureVault</h1>
                    <div className="flex items-center gap-4">
                        <span className="text-sm text-muted-foreground">{user?.email}</span>
                        <Button variant="outline" size="sm" onClick={logout}>
                            Logout
                        </Button>
                    </div>
                </div>
            </header>

            <main className="mx-auto max-w-5xl p-4">
                <div className="mb-6 flex items-center justify-between">
                    <h2 className="text-2xl font-bold">Meine Secrets</h2>
                    <Button onClick={handleCreate}>+ Neues Secret</Button>
                </div>

                {error && (
                    <div className="mb-4 rounded-md bg-destructive/10 p-3 text-sm text-destructive">
                        {error}
                    </div>
                )}

                {isLoading ? (
                    <p className="text-muted-foreground">Laden...</p>
                ) : secrets.length === 0 ? (
                    <p className="text-muted-foreground">
                        Noch keine Secrets vorhanden. Erstelle dein erstes Secret!
                    </p>
                ) : (
                    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {secrets.map((secret) => (
                            <SecretCard
                                key={secret.id}
                                secret={secret}
                                onView={() => handleView(secret.id)}
                                onEdit={() => handleEdit(secret.id)}
                                onDelete={() => handleDelete(secret.id)}
                            />
                        ))}
                    </div>
                )}

                <SecretModal
                    open={modalOpen}
                    onClose={() => {
                        setModalOpen(false);
                        setEditingSecret(null);
                    }}
                    onSave={handleSave}
                    secret={editingSecret}
                />
            </main>
        </div>
    );
}
