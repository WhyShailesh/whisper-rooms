/**
 * Vanish backend: Node.js + Socket.IO
 * - Messages exist only in RAM during relay; never written to disk or database.
 * - username -> socketId map for direct messages.
 * - roomCode -> room object for room chat; admin approves joins; room deleted when admin leaves.
 */

const http = require('http');
const { Server } = require('socket.io');

const PORT = process.env.PORT || 3000;

const server = http.createServer();
const io = new Server(server, {
  cors: { origin: '*' },
  transports: ['websocket']
});

// In-memory only: no DB, no file storage
const usernameToSocketId = new Map(); // username (lowercase) -> socket.id
const socketIdToUsername = new Map(); // socket.id -> username
const rooms = new Map(); // roomCode -> { adminSocketId, members: Set<socketId>, pending: Set<username> }

function generateRoomCode() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let code = '';
  for (let i = 0; i < 6; i++) code += chars[Math.floor(Math.random() * chars.length)];
  return code;
}

io.on('connection', (socket) => {
  socket.on('register', (username) => {
    const normalized = (username || '').toString().trim().toLowerCase();
    if (!normalized) return;
    usernameToSocketId.set(normalized, socket.id);
    socketIdToUsername.set(socket.id, normalized);
    socket.username = normalized;
    socket.emit('register_ack', { socketId: socket.id });
  });

  socket.on('direct_message', (payload) => {
    const to = payload?.to?.trim?.()?.toLowerCase?.();
    const text = (payload?.text ?? '').toString();
    const from = socket.username;
    if (!to || !from || !text) return;
    const targetSocketId = usernameToSocketId.get(to);
    if (!targetSocketId) return; // offline -> drop
    io.to(targetSocketId).emit('direct_message', { from, text });
  });

  socket.on('create_room', (_, cb) => {
    const admin = socket.username;
    if (!admin) return cb?.({ error: 'Not registered' });
    let code = generateRoomCode();
    while (rooms.has(code)) code = generateRoomCode();
    rooms.set(code, {
      adminSocketId: socket.id,
      members: new Set([socket.id]),
      pending: new Set()
    });
    socket.roomCode = code;
    cb?.({ roomCode: code });
  });

  socket.on('join_room', (payload, cb) => {
    const code = (payload?.roomCode ?? '').toString().trim().toUpperCase();
    const user = socket.username;
    if (!user || !code) return cb?.({ error: 'Invalid request' });
    const room = rooms.get(code);
    if (!room) return cb?.({ error: 'Room not found' });
    if (room.members.has(socket.id)) return cb?.({ status: 'joined' });
    room.pending.add(user);
    socket.roomCodePending = code;
    io.to(room.adminSocketId).emit('join_request', { roomCode: code, username: user });
    cb?.({ status: 'pending' });
  });

  socket.on('approve_join', (payload) => {
    const code = (payload?.roomCode ?? '').toString().trim().toUpperCase();
    const username = (payload?.username ?? '').toString().trim().toLowerCase();
    const room = rooms.get(code);
    if (!room || room.adminSocketId !== socket.id) return;
    const targetSocketId = usernameToSocketId.get(username);
    if (!targetSocketId) return;
    room.pending.delete(username);
    room.members.add(targetSocketId);
    io.sockets.sockets.get(targetSocketId).roomCode = code;
    io.to(targetSocketId).emit('join_room_ack', { roomCode: code, status: 'joined' });
    const memberList = [...room.members].map(id => socketIdToUsername.get(id)).filter(Boolean);
    room.members.forEach(id => io.to(id).emit('room_members', { roomCode: code, members: memberList }));
  });

  socket.on('leave_room', (payload) => {
    const code = (payload?.roomCode ?? '').toString().trim().toUpperCase();
    const room = rooms.get(code);
    if (!room) return;
    room.members.delete(socket.id);
    socket.roomCode = null;
    const memberList = [...room.members].map(id => socketIdToUsername.get(id)).filter(Boolean);
    room.members.forEach(id => io.to(id).emit('room_members', { roomCode: code, members: memberList }));
    if (room.adminSocketId === socket.id) {
      room.members.forEach(id => io.to(id).emit('room_closed', { roomCode: code }));
      rooms.delete(code);
    }
  });

  socket.on('room_message', (payload) => {
    const code = (payload?.roomCode ?? '').toString().trim().toUpperCase();
    const text = (payload?.text ?? '').toString();
    const from = socket.username;
    if (!code || !from || !text) return;
    const room = rooms.get(code);
    if (!room || !room.members.has(socket.id)) return;
    room.members.forEach(id => {
      if (id !== socket.id) io.to(id).emit('room_message', { roomCode: code, from, text });
    });
  });

  socket.on('disconnect', () => {
    const username = socketIdToUsername.get(socket.id);
    usernameToSocketId.delete(username);
    socketIdToUsername.delete(socket.id);
    const code = socket.roomCode || socket.roomCodePending;
    if (code) {
      const room = rooms.get(code);
      if (room) {
        room.members.delete(socket.id);
        room.pending.delete(username);
        const memberList = [...room.members].map(id => socketIdToUsername.get(id)).filter(Boolean);
        room.members.forEach(id => io.to(id).emit('room_members', { roomCode: code, members: memberList }));
        if (room.adminSocketId === socket.id) {
          room.members.forEach(id => io.to(id).emit('room_closed', { roomCode: code }));
          rooms.delete(code);
        }
      }
    }
  });
});

server.listen(PORT, () => {
  console.log(`Vanish backend listening on port ${PORT}`);
});
