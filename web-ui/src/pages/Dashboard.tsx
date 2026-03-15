import { Container, Typography, Button } from "@mui/material";
import { signOut } from "firebase/auth";
import { auth } from "../lib/firebase";
import { useAuth } from "../app/contexts/AuthContext";

export default function Dashboard() {

  const { user, token } = useAuth();

  return (
    <Container>

      <Typography variant="h4" mt={5}>
        Welcome {user?.email}
      </Typography>

      <Typography mt={2}>
        Firebase Bearer Token (first 40 chars)
      </Typography>

      <Typography sx={{ wordBreak: "break-all" }}>
        {token?.slice(0, 40)}...
      </Typography>

      <Button
        sx={{ mt: 4 }}
        variant="outlined"
        onClick={() => signOut(auth)}
      >
        Logout
      </Button>

    </Container>
  );
}
