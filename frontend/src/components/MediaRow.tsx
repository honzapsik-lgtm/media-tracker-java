'use client';

import { MediaItem } from '@/types';
import MediaCardVertical from './MediaCardVertical';
import Carousel from './Carousel';

export default function MediaRow({ title, items }: { title: string; items: MediaItem[] }) {
  if (!items || items.length === 0) return null;

  return (
    <div className="mb-14">
      <h2 className="text-2xl font-bold mb-4 tracking-wide text-gray-100">{title}</h2>
      <Carousel>
        {items.map((item) => (
          <div key={item.id} className="flex-none w-40 sm:w-48 lg:w-56 snap-start">
            <MediaCardVertical item={item} />
          </div>
        ))}
      </Carousel>
    </div>
  );
}
