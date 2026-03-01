import { useEffect, useState } from 'react';
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import type { SecretResponse, SecretType } from '@/types/vault';

interface SecretModalProps {
    open: boolean;
    onClose: () => void;
    onSave: (data: { name: string; value: string; description: string; secretType: SecretType }) => void;
    secret: SecretResponse | null;
}

const SECRET_TYPES: SecretType[] = ['PASSWORD', 'API_KEY', 'NOTE', 'CERTIFICATE', 'OTHER'];

export default function SecretModal({ open, onClose, onSave, secret }: SecretModalProps) {
    const [name, setName] = useState('');
    const [value, setValue] = useState('');
    const [description, setDescription] = useState('');
    const [secretType, setSecretType] = useState<SecretType>('PASSWORD');

    useEffect(() => {
        if (secret) {
            setName(secret.name);
            setValue(secret.value);
            setDescription(secret.description || '');
            setSecretType(secret.secretType);
        } else {
            setName('');
            setValue('');
            setDescription('');
            setSecretType('PASSWORD');
        }
    }, [secret, open]);

    const handleSubmit = () => {
        if (!name.trim() || !value.trim()) return;
        onSave({ name, value, description, secretType });
    };

    return (
        <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>
                        {secret ? 'Secret bearbeiten' : 'Neues Secret'}
                    </DialogTitle>
                </DialogHeader>
                <div className="space-y-4 py-4">
                    <div className="space-y-2">
                        <Label htmlFor="name">Name</Label>
                        <Input
                            id="name"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            placeholder="z.B. GitHub Token"
                        />
                    </div>
                    <div className="space-y-2">
                        <Label htmlFor="secretType">Typ</Label>
                        <select
                            id="secretType"
                            value={secretType}
                            onChange={(e) => setSecretType(e.target.value as SecretType)}
                            className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs"
                        >
                            {SECRET_TYPES.map((type) => (
                                <option key={type} value={type}>{type}</option>
                            ))}
                        </select>
                    </div>
                    <div className="space-y-2">
                        <Label htmlFor="value">Wert</Label>
                        <Textarea
                            id="value"
                            value={value}
                            onChange={(e) => setValue(e.target.value)}
                            placeholder="Geheimer Wert..."
                        />
                    </div>
                    <div className="space-y-2">
                        <Label htmlFor="description">Beschreibung (optional)</Label>
                        <Input
                            id="description"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                        />
                    </div>
                </div>
                <DialogFooter>
                    <Button variant="outline" onClick={onClose}>Abbrechen</Button>
                    <Button onClick={handleSubmit}>Speichern</Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
