import { useEffect, useRef } from 'react'
import { Group, Rect, Text, Transformer } from 'react-konva'
import type Konva from 'konva'
import type { Room } from '../types'

const colors = { CARDIO:'#79c8d0', WEIGHTS:'#baf252', GROUP_STUDIO:'#c8a6ff', FUNCTIONAL:'#ffb86b', LOCKER_ROOM:'#8ca6b3', OTHER:'#dde4dc' }
const minimumRoomSize = (name: string) => ({ width: Math.max(100, name.trim().length * 10 + 32), height: 80 })

interface Props {
  room: Room
  selected: boolean
  onSelect: () => void
  onChange: (room: Room, persist: boolean) => void
}

export function RoomShape({ room, selected, onSelect, onChange }: Props) {
  const shapeRef = useRef<Konva.Group>(null)
  const transformerRef = useRef<Konva.Transformer>(null)
  useEffect(() => {
    if (selected && shapeRef.current && transformerRef.current) {
      transformerRef.current.nodes([shapeRef.current]); transformerRef.current.getLayer()?.batchDraw()
    }
  }, [selected])
  return <>
    <Group ref={shapeRef} x={room.posX} y={room.posY} width={room.width} height={room.height} rotation={room.rotationDegrees}
      draggable onClick={onSelect} onTap={onSelect}
      onDragMove={(e)=>onChange({...room,posX:e.target.x(),posY:e.target.y()},false)}
      onDragEnd={(e)=>onChange({...room,posX:e.target.x(),posY:e.target.y()},true)}
      onTransformEnd={() => {
        const node=shapeRef.current!; const scaleX=node.scaleX(); const scaleY=node.scaleY(); node.scaleX(1);node.scaleY(1)
        const minimum=minimumRoomSize(room.name)
        onChange({...room,posX:node.x(),posY:node.y(),rotationDegrees:node.rotation(),width:Math.max(minimum.width,room.width*scaleX),height:Math.max(minimum.height,room.height*scaleY)},true)
      }}>
      <Rect width={room.width} height={room.height} cornerRadius={16} fill={colors[room.type]} stroke={selected?'#152319':'#ffffff'} strokeWidth={selected?3:2} shadowColor="#18221c" shadowOpacity={.12} shadowBlur={12} shadowOffsetY={5}/>
      <Text x={16} y={17} width={Math.max(20,room.width-32)} text={room.name.toUpperCase()} fontFamily="Manrope" fontStyle="bold" fontSize={Math.min(18,Math.max(11,room.width/10))} fill="#152319" ellipsis/>
      <Text x={16} y={42} width={Math.max(20,room.width-32)} text={`${room.capacity} mesta · ${room.type.replace('_',' ')}`} fontFamily="DM Sans" fontSize={11} fill="#3b4a40"/>
    </Group>
    {selected && <Transformer ref={transformerRef} rotateEnabled enabledAnchors={['top-left','top-right','bottom-left','bottom-right','middle-left','middle-right','top-center','bottom-center']} anchorFill="#baf252" anchorStroke="#152319" anchorSize={10} borderStroke="#152319" boundBoxFunc={(oldBox,newBox)=>{const minimum=minimumRoomSize(room.name);return newBox.width<minimum.width||newBox.height<minimum.height?oldBox:newBox}}/>}
  </>
}
