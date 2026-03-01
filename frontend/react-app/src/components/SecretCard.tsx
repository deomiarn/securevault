import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import type { SecretSummaryResponse } from '@/types/vault';

interface SecretCardProps {
    secret: SecretSummaryResponse;
    onEdit: () => void;
    onDelete: () => void;
    onView: () => void;
}

export default function SecretCard({ secret, onEdit, onDelete, onView }: SecretCardProps) {
    const [confirmDelete, setConfirmDelete] = useState(false);

    const handleDelete = () => {
        if (confirmDelete) {
            onDelete();
            setConfirmDelete(false);
        } else {
            setConfirmDelete(true);
            setTimeout(() => setConfirmDelete(false), 3000);
        }
    };

    return (
        <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-lg">{secret.name}</CardTitle>
                <Badge variant="secondary">{secret.secretType}</Badge>
            </CardHeader>
            <CardContent>
                <div className="mb-3 text-sm text-muted-foreground">
                    {secret.folderName && <span>Ordner: {secret.folderName}</span>}
                    <span className="ml-2">
                        {new Date(secret.createdAt).toLocaleDateString()}
                    </span>
                </div>
                <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={onView}>
                        Anzeigen
                    </Button>
                    <Button variant="outline" size="sm" onClick={onEdit}>
                        Bearbeiten
                    </Button>
                    <Button
                        variant={confirmDelete ? 'destructive' : 'outline'}
                        size="sm"
                        onClick={handleDelete}
                    >
                        {confirmDelete ? 'Sicher?' : 'Loeschen'}
                    </Button>
                </div>
            </CardContent>
        </Card>
    );
}
